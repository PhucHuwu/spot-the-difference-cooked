package com.ltm.game.shared;

public final class Protocol {
    private Protocol() {}

    public static final String AUTH_LOGIN = "auth/login";
    public static final String AUTH_RESULT = "auth/result";

    public static final String ERROR = "error";

    public static final String LOBBY_LIST = "lobby/list";
    public static final String LOBBY_REQUEST = "lobby/request";
    public static final String INVITE_SEND = "invite/send";
    public static final String INVITE_RECEIVED = "invite/received";
    public static final String INVITE_RESPONSE = "invite/response";

    public static final String QUEUE_JOIN = "queue/join";
    public static final String QUEUE_LEAVE = "queue/leave";
    public static final String QUEUE_STATUS = "queue/status";
    public static final String QUEUE_MATCHED = "queue/matched";

    public static final String GAME_START = "game/start";
    public static final String GAME_CLICK = "game/click";
    public static final String GAME_UPDATE = "game/update";
    public static final String GAME_END = "game/end";
    public static final String GAME_QUIT = "game/quit";

    public static final String TIMER_TICK = "timer/tick";

    public static final String LEADERBOARD = "leaderboard/data";
}

