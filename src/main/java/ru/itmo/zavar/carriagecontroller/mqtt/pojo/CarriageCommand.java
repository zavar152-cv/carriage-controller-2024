package ru.itmo.zavar.carriagecontroller.mqtt.pojo;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public final class CarriageCommand<T> {
    private final String command;
    private T argument;
}
