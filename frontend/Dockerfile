# syntax=docker/dockerfile:1

########## 构建阶段 ##########
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm config set registry https://registry.npmmirror.com && npm install
COPY . .
RUN npm run build

########## 运行阶段 ##########
FROM nginx:alpine AS runtime
# 以非 root 用户 nginx 运行：使用自定义主配置（监听 8080、临时目录放 /tmp）
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /build/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1
CMD ["nginx", "-g", "daemon off;"]
