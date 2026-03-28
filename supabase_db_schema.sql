-- Скрипт создания таблиц в Supabase SQL Editor

-- 1. Таблица Проектов (Projects)
CREATE TABLE projects (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  owner_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Таблица Дорожек (Tracks)
CREATE TABLE project_tracks (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  project_id UUID REFERENCES projects(id) ON DELETE CASCADE NOT NULL,
  name TEXT NOT NULL,
  file_path TEXT NOT NULL, -- Путь к файлу внутри Supabase Storage
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Устанавливаем базовые параметры RLS
ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_tracks ENABLE ROW LEVEL SECURITY;

-- 3. RLS Политики для Проектов (Пользователь видит и редактирует только свои проекты)
CREATE POLICY "Users can view own projects" ON projects FOR SELECT USING (auth.uid() = owner_id);
CREATE POLICY "Users can insert own projects" ON projects FOR INSERT WITH CHECK (auth.uid() = owner_id);
CREATE POLICY "Users can update own projects" ON projects FOR UPDATE USING (auth.uid() = owner_id);
CREATE POLICY "Users can delete own projects" ON projects FOR DELETE USING (auth.uid() = owner_id);

-- 4. RLS Политики для Треков (Пользователь имеет доступ к трекам только своих проектов)
CREATE POLICY "Users can view tracks of own projects" ON project_tracks FOR SELECT USING (
  EXISTS (SELECT 1 FROM projects WHERE projects.id = project_tracks.project_id AND projects.owner_id = auth.uid())
);
CREATE POLICY "Users can insert tracks to own projects" ON project_tracks FOR INSERT WITH CHECK (
  EXISTS (SELECT 1 FROM projects WHERE projects.id = project_tracks.project_id AND projects.owner_id = auth.uid())
);
CREATE POLICY "Users can update tracks of own projects" ON project_tracks FOR UPDATE USING (
  EXISTS (SELECT 1 FROM projects WHERE projects.id = project_tracks.project_id AND projects.owner_id = auth.uid())
);
CREATE POLICY "Users can delete tracks of own projects" ON project_tracks FOR DELETE USING (
  EXISTS (SELECT 1 FROM projects WHERE projects.id = project_tracks.project_id AND projects.owner_id = auth.uid())
);

-- =============================================================================
-- МИГРАЦИЯ: Workspaces и шеринг
-- Выполнить в Supabase Dashboard → SQL Editor
-- =============================================================================

-- 5. Таблица Воркспейсов
CREATE TABLE workspaces (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  owner_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  share_token UUID UNIQUE DEFAULT NULL, -- NULL = приватный, генерируется по запросу
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Таблица участников воркспейса (joined по ссылке)
CREATE TABLE workspace_members (
  workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE NOT NULL,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  joined_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (workspace_id, user_id)
);

-- 7. Добавить workspace_id в projects
ALTER TABLE projects
  ADD COLUMN workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE;

-- Временный индекс для быстрого поиска проектов воркспейса
CREATE INDEX idx_projects_workspace_id ON projects(workspace_id);

-- =============================================================================
-- RLS для workspaces
-- =============================================================================
ALTER TABLE workspaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspace_members ENABLE ROW LEVEL SECURITY;

-- Владелец видит свои воркспейсы
CREATE POLICY "Owners can view own workspaces"
  ON workspaces FOR SELECT
  USING (auth.uid() = owner_id);

-- Участник (joined) видит воркспейс
CREATE POLICY "Members can view joined workspaces"
  ON workspaces FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM workspace_members
      WHERE workspace_members.workspace_id = workspaces.id
        AND workspace_members.user_id = auth.uid()
    )
  );

-- Любой (включая анонима) может прочитать воркспейс по share_token
CREATE POLICY "Anyone can view workspace by share_token"
  ON workspaces FOR SELECT
  USING (share_token IS NOT NULL);

-- Владелец создаёт воркспейсы
CREATE POLICY "Owners can insert workspaces"
  ON workspaces FOR INSERT
  WITH CHECK (auth.uid() = owner_id);

-- Владелец обновляет свои воркспейсы (переименование, генерация share_token)
CREATE POLICY "Owners can update own workspaces"
  ON workspaces FOR UPDATE
  USING (auth.uid() = owner_id);

-- Владелец удаляет свои воркспейсы
CREATE POLICY "Owners can delete own workspaces"
  ON workspaces FOR DELETE
  USING (auth.uid() = owner_id);

-- =============================================================================
-- RLS для workspace_members
-- =============================================================================

-- Участник видит запись о своём членстве
CREATE POLICY "Members can view own membership"
  ON workspace_members FOR SELECT
  USING (auth.uid() = user_id);

-- Авторизованный пользователь может вступить (join) в воркспейс
CREATE POLICY "Authenticated users can join workspaces"
  ON workspace_members FOR INSERT
  WITH CHECK (auth.uid() = user_id AND auth.uid() IS NOT NULL);

-- Участник может выйти из воркспейса
CREATE POLICY "Members can leave workspaces"
  ON workspace_members FOR DELETE
  USING (auth.uid() = user_id);

-- =============================================================================
-- Обновлённые RLS для projects (учитываем workspace)
-- =============================================================================

-- Участник воркспейса (или аноним по share_token) видит проекты воркспейса
CREATE POLICY "Workspace members can view projects"
  ON projects FOR SELECT
  USING (
    -- Свои проекты
    auth.uid() = owner_id
    OR
    -- Проекты воркспейса, в котором юзер является участником
    EXISTS (
      SELECT 1 FROM workspace_members
      WHERE workspace_members.workspace_id = projects.workspace_id
        AND workspace_members.user_id = auth.uid()
    )
    OR
    -- Проекты воркспейса с активным share_token (гостевой доступ)
    EXISTS (
      SELECT 1 FROM workspaces
      WHERE workspaces.id = projects.workspace_id
        AND workspaces.share_token IS NOT NULL
    )
  );

-- Треки: аналогично расширяем доступ через workspace
CREATE POLICY "Workspace members can view tracks"
  ON project_tracks FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM projects
      WHERE projects.id = project_tracks.project_id
        AND (
          projects.owner_id = auth.uid()
          OR EXISTS (
            SELECT 1 FROM workspace_members
            WHERE workspace_members.workspace_id = projects.workspace_id
              AND workspace_members.user_id = auth.uid()
          )
          OR EXISTS (
            SELECT 1 FROM workspaces
            WHERE workspaces.id = projects.workspace_id
              AND workspaces.share_token IS NOT NULL
          )
        )
    )
  );

-- =============================================================================
-- Trigger: создать дефолтный воркспейс при регистрации пользователя
-- =============================================================================

CREATE OR REPLACE FUNCTION create_default_workspace()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.workspaces (name, owner_id)
  VALUES ('My Workspace', NEW.id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION create_default_workspace();

-- =============================================================================
-- Supabase Storage: разрешить анонимный доступ к трекам по share_token
-- (Настраивается в Dashboard → Storage → Policies, см. инструкции)
-- =============================================================================
