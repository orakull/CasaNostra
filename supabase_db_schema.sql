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
