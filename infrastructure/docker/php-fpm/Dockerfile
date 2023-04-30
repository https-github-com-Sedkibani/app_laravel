FROM php:8.1-fpm

ADD . /var/www/html
WORKDIR /var/www/html
RUN mkdir -p /var/www/html/storage/framework/cache
RUN mkdir -p /var/www/html/storage/framework/views
RUN mkdir -p /var/www/html/storage/framework/sessions
RUN rm -rf   /var/www/html/.git
RUN rm -rf   /var/www/html/bootstrap/cache/*
RUN chown -R www-data:www-data /var/www/html

COPY ./infrastructure/docker/php-fpm/config/php_config.ini /usr/local/etc/php/conf.d/php_config.ini
COPY ./infrastructure/docker/php-fpm/config/www.conf /usr/local/etc/php-fpm.d/www.conf
