# Зеркалируем вордпресовский сайт в локальную директорию.

Этот проект — аналог команды:
```shell
wget -mEpnp <какой-то сайт>
```
— для зеркалирования какого-то сайта в статические файлы на локальной файловой системе. 
Во время зеркалирования внутренние ссылки в файлах html, css или js переписываются на относительные,
чтобы указывать (соседние) на ресурсы зеркала.

## Сборка проекта
Когда скачаете проект, скопируйте файлы:
```
src/main/resources/main.properties.example
src/main/resources/slider.properties.example
src/main/resources/vault.properties.example
```
рядом, удалив у них расширение `.example`, и настройте их под свои нужды. После этого сборка проекта

```shell
./gradlew check assemble
```
должна проходить.

Вдобавок к зеркалированию сайта, изображения `.jpg` и `.png` оттуда дополнительно сохраняются в формате 
`.wepb`. Для этого в настройках надо указать путь к 
[гугловой утилите `cwebp`](https://developers.google.com/speed/webp/docs/precompiled) для 
конвертирования изображений.

## Запуск
Производится либо вызовом
```shell
./gradlew run
```
либо созданием дистрибутива
```shell
./gradlew distTar
```
(смотри результат в `build/distributions`) и запуска скрипта из распакованного архива.

## Результат
Статические страницы складываются в директорию, откуда их можно просмотреть браузером локально. Можно скопировать
содержимое на сервер и раздавать оттуда как статические страницы сайта с помощью `Nginx` или `Apache`. Пример
конфигурации `Nginx` включен в репозиторий. Такая конфигурация позволяет отдельно обрабатывать динамические вызовы
(отправку данных через формы). Позволяет при запросе картинок отдавать файлы в формате webp, если браузер это
поддерживает. `Nginx` играет роль SSL и HTTP2 терминатора, он перенаправляет обращения к сайту по псевдониму или
по незащищённому протоколу на нужный домен через HTTPS.

## Лицензия
Апачевская.
