package team.luxinfine.rslauncher.client;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Имплементация API лаунчера для клиентской части через JNI обращение к реализации лаунчера<HR>
 * <B>ВНИМАНИЕ:</B> Данный класс всегда загружается в BootClassLoader, чтобы быть доступным для всех загрузчиков классов
 * любых платформ (fabric, forge, neo-forge, etc), потому в сигнатурах методов <B>ЗАПРЕЩЕНО</B> использование
 * классов, не входящих в стандартный java SDK. Их наличие приведет к ClassNotFound ошибкам<HR>
 * Дело в том, что некоторые модлоадеры для своих загрузчиков классов указали null в качестве родителя либо вообще DUMMY (в fabric),
 * из-за чего загрузить этот класс просто в -cp недостаточно, игра тупо не увидит его, либо загрузит КОПИЮ класса в свой загрузчик,
 * где native методы не будут иметь имплементации. Bootloader работает всегда, независимо от реализации класслоудеров.
 * @see team.luxinfine.authlib.CommonAPI
 */
public final class NativeAPI {

    //Блокирующий запрос начала подключения к майнкрафт серверу
    //serverID - сгенерированный ID входа игровым клиентом
    public static native void joinMCServer(@Nonnull String serverID) throws TimeoutException, RuntimeException;

    //Блокирующий запрос игровых профилей
    //users - список ников/uuid игроков для поиска
    //by_uuid - если true - в списке должны быть UUID (в виде строк с тире), иначе должны быть ники
    //Возвращает Map, где ключи - значения из users списка, а значения - Map параметров гейм профиля игрока
    //-----------
    //Структура профиля в виде Map:
    //id - UUID игрока строкой с тире
    //name - никнейм игрока
    //properties - список Property геймпрофиля (List<Map<String,String>>)
    //---------
    //Структура Property в виде Map:
    //name - имя проперти
    //value - значение проперти
    @Nonnull
    public static native Map<String, Map<String, Object>> lookupProfiles(@Nonnull List<String> users, boolean by_uuid) throws TimeoutException, RuntimeException;
}
