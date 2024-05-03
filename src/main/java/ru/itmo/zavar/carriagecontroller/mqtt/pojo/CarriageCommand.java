package ru.itmo.zavar.carriagecontroller.mqtt.pojo;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public final class CarriageCommand<T> {
    private final String command;
    private final Byte type;
    private T argument;

    public static CarriageCommand<Void> getEmptyCommand() {
        return new CarriageCommand<>("", (byte) 0, null);
    }

    public static boolean isEmpty(CarriageCommand<?> carriageCommand) {
        return carriageCommand.getCommand().isEmpty() && carriageCommand.getArgument() == null;
    }
}
