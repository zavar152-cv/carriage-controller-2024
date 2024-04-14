package ru.itmo.zavar.carriagecontroller.mqtt.pojo;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public final class CarriageCommand<T> {
    private final String command;
    private T argument;

    public static CarriageCommand<Void> getEmptyCommand() {
        return new CarriageCommand<>("", null);
    }

    public static boolean isEmpty(CarriageCommand<?> carriageCommand) {
        return carriageCommand.getCommand().isEmpty() && carriageCommand.getArgument() == null;
    }
}
