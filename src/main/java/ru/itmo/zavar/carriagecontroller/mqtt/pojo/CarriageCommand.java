package ru.itmo.zavar.carriagecontroller.mqtt.pojo;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
public final class CarriageCommand<T> {
    private final String command;
    private T argument;
}
