FROM nginx:1.19.6
ADD . /var/www/html
COPY vendor /var/www/html/vendor
ADD ./composer.lock /var/www/html/composer.lock
WORKDIR /var/www/html
RUN rm -rf /var/www/html/.git
RUN rm -rf /var/www/html/bootstrap/cache/*

#ADD ./config/server.conf /etc/nginx/conf.d/default.conf
#ADD ./config/nginx.conf /etc/nginx/nginx.conf
ADD ./infrastructure/docker/nginx/config/server.conf /etc/nginx/conf.d/default.conf
ADD ./infrastructure/docker/nginx/config/nginx.conf /etc/nginx/nginx.conf
