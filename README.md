# Timer Mod (Fabric 1.20.1)

Compilar (necesita Java 17 y Gradle 8.6+):
    gradle wrapper --gradle-version 8.6
    ./gradlew build
El .jar queda en build/libs/timermod-1.0.0.jar

Instalar el MISMO .jar en el servidor y en el cliente de cada jugador (con Fabric Loader + Fabric API 1.20.1).
Comandos (nivel op 2): /timer <20s|20m|20hs>, /timer stop, /timerplayer <jugador> <tiempo|stop>,
/vanish, /unvanish, /tagvanish, /untagvanish. Items: en la pestaña creativa "Timer Mod"
(o /give @s timermod:a_casa_pa, timermod:tp_cero, timermod:ichigo_sword).
