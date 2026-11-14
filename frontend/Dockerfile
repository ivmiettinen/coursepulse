FROM node:24.22.3-bookworm-slim AS build
WORKDIR /app/frontend
# The build context has no Git repository for the Husky prepare hook.
ENV HUSKY=0
COPY package.json package-lock.json ./
RUN npm ci
COPY angular.json tsconfig*.json ./
COPY src ./src
COPY public ./public
RUN npm run build -- --configuration production

FROM caddy:2.11.9-alpine AS runtime
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/frontend/dist/coursepulse/browser /srv
EXPOSE 80
