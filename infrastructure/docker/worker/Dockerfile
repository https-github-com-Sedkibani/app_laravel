FROM rg.fr-par.scw.cloud/base/php-worker-prod:1.0.0

ADD . /var/www/html
WORKDIR /var/www/html
RUN mkdir -p /var/www/html/storage/framework/cache
RUN mkdir -p /var/www/html/storage/framework/views
RUN mkdir -p /var/www/html/storage/framework/sessions
RUN rm -rf   /var/www/html/.git
RUN rm -rf   /var/www/html/bootstrap/cache/*
RUN chown -R www-data:www-data /var/www/html
COPY ./infrastructure/docker/php-fpm/config/php_config.ini /usr/local/etc/php/conf.d/