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
src/main/resources/sliderRepo.properties.example
src/main/resources/vault.properties.example
```
рядышком, удалив у них расширение `.example` и настройте их под свои нужды. После этого сборка проекта

```shell
./gradlew check assemble
```
должна проходить.
