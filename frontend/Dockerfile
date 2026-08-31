# Multi-stage build for React + Vite frontend
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

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
