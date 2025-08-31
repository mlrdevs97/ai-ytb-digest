FROM nginx:1.27-alpine
COPY src/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/nginx.conf
RUN rm -f /etc/nginx/conf.d/*

EXPOSE 80
