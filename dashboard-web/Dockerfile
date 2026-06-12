FROM nginx

COPY /deploy /usr/share/nginx/html/js
COPY /deploy/index.html /usr/share/nginx/html/index.html
COPY /css /usr/share/nginx/html/css
COPY /fontawesome-free-5.0.6 /usr/share/nginx/html/fontawesome-free-5.0.6
COPY /jsap /usr/share/nginx/html/jsap
COPY docker_entrypoint.sh /usr/share/nginx/html/docker_entrypoint.sh
COPY icon /usr/share/nginx/html/icon
RUN chmod +x /usr/share/nginx/html/docker_entrypoint.sh
RUN chmod +r "/usr/share/nginx/html/icon/MilkDataWay_Icon_ White.svg"

ENTRYPOINT [ "/usr/share/nginx/html/docker_entrypoint.sh" ]
