FROM nginx:stable-alpine

# Copy the static build output (populated by CI before docker build)
COPY dist/ /usr/share/nginx/html/

# SPA routing + wasm MIME type
COPY nginx-spa.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
