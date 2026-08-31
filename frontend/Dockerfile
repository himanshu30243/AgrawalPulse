# Multi-stage build for React + Vite frontend
# Stage 1: Build
# 22, not 20: @testing-library/jest-dom (and possibly others) require >=22 - kept in step with
# .github/workflows/ci-cd.yml's node-version.
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# npm install, not npm ci: package-lock.json is maintained from Windows dev machines, and npm
# has a known bug (npm/cli#4828) where a lock file generated on one platform can omit/mismatch
# the platform-specific optional native bindings (e.g. @rolldown/binding-*) another platform
# needs - npm ci trusts the lock file's cross-platform completeness and fails outright when
# it's wrong; npm install re-resolves live against whatever platform it's actually running on
# (here: Linux/musl, not Windows) and self-heals instead.
RUN npm install

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Stage 2: Runtime (Nginx)
FROM nginx:alpine

# Copy built assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Template, not a static conf: nginx's own docker-entrypoint.sh runs envsubst on every
# file under /etc/nginx/templates/*.template at container start and writes the result to
# conf.d/ - that's what actually resolves ${API_GATEWAY_HOST} in nginx.conf.template.
# A default keeps `docker run` without -e usable for a quick local smoke test.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV API_GATEWAY_HOST=localhost:8090

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/health || exit 1

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
