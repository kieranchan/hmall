# 使用官方 nginx 镜像
FROM nginx:1.24-alpine

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义配置
COPY conf/nginx.conf /etc/nginx/nginx.conf

# 复制前端静态文件
COPY html /usr/share/nginx/html

# 暴露端口
EXPOSE 18080 18081 18082

# 启动 nginx
CMD ["nginx", "-g", "daemon off;"]
