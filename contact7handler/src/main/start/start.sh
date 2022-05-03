#!/usr/bin/env bash
PROJ_HOME=/var/log/contact7handler
cd ${PROJ_HOME} || exit 1

###############################################
# определяем переменные, которые попадут в JDK_JAVA_OPTIONS и будут _скрыты_ от утилит top, ps и так далее
###############################################

JAVAX_NET_DEBUG=""
[ -n "$JAVAX_NET_DEBUG_ITEMS" ] && JAVAX_NET_DEBUG="-Djavax.net.debug=${JAVAX_NET_DEBUG_ITEMS}"

IFS='' read -d '' -r OOM_ACTIONS <<'HEREDOC'
-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=${PROJ_HOME}
HEREDOC

IFS='' read -d '' -r MISC_OPTS <<'HEREDOC'
-Dfile.encoding=UTF-8
-Djava.net.preferIPv4Stack=true
HEREDOC

CP='-cp .:/lib/*'

JDK_JAVA_OPTIONS="$CP $OOM_ACTIONS $MISC_OPTS $JAVAX_NET_DEBUG"
JDK_JAVA_OPTIONS=${JDK_JAVA_OPTIONS//$'\n'/ }
export JDK_JAVA_OPTIONS

###############################################
# определяем переменные, которые попадут в параметры запуска и _будут_ видны в утилитах top, ps и так далее
###############################################

# на всякий случай укажем здесь значения по умолчанию
: "${XMS_XMX:=512m}"
: "${DIRECT_MEMSIZE:=50m}"
: "${METASPACE_MEMSIZE:=50m}"
: "${DEBUG_SUSPEND:=n}"

if [ -n "$JDWP_PORT" ]; then
  case $JDWP_PORT in
  localhost:*) ;;

  127.0.0.1:*) ;;

  *)
    JDWP_PORT="localhost:${JDWP_PORT}"
    ;;
  esac
  DEBUG_LINE="-agentlib:jdwp=transport=dt_socket,server=y,suspend=${DEBUG_SUSPEND},address=${JDWP_PORT}"
else
  DEBUG_LINE=""
fi

if [ -n "$JMX_PORT" ]; then
  IFS='' read -d '' -r JMX_LINE <<HEREDOC
-Dcom.sun.management.jmxremote.port=${JMX_PORT}
-Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT}
-Djava.rmi.server.hostname=127.0.0.1
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
HEREDOC
  JMX_LINE=${JMX_LINE//$'\n'/ }
else
  JMX_LINE=""
fi

###############################################
# вспомогательные функции
###############################################

mod640() {
  [ -f "$1" ] || return
  chown @USER@:@LOG_GROUP@ "$1"
  chmod 640 "$1"
}

archive() {
  # move FILE to FILE.YYYY-MM-DD.HH-MM
  if [ -f "$1" ]; then
    local NOW
    NOW=$(date +%Y-%m-%d.%H-%M)
    local NEW_FILE="$1.$NOW"
    [ -f "$NEW_FILE" ] && NEW_FILE="$NEW_FILE.$RANDOM"
    echo "move $1 to $NEW_FILE"
    mv "$1" "${NEW_FILE}"
    mod640 "${NEW_FILE}"
  fi
  touch "$1"
  mod640 "$1"
}

# логируем в syslog команду с параметрами запуска
execRunner() {
  A=${*//-/⁃} # в syslog не позволено писать параметры, которые начинаются с "-"
  logger "# Executing command line: ${A}"
  exec "$@"
}

###############################################
# архивируем логи предыдущего запуска и запускаем Яву
###############################################

CONSOLE_OUT=$PROJ_HOME/console.out
archive "$CONSOLE_OUT"

echo 'starting @DESCRIPTION@ of version @APP_VERSION@' >"$CONSOLE_OUT"

# для отладки надо добавить флаг -Xdiag
execRunner java \
  -Xms${XMS_XMX} -Xmx${XMS_XMX} -XX:MaxDirectMemorySize=${DIRECT_MEMSIZE} -XX:MaxMetaspaceSize=${METASPACE_MEMSIZE} \
  $DEBUG_LINE $JMX_LINE \
  "aikisib.contact7.MainContact7Kt" \
  >>"$CONSOLE_OUT" 2>&1
