package ru.itmo.zavar.carriagecontroller.mqtt.pojo;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public final class CarriageCommand<T> {
    private String command;
    private Byte type;
    private T argument;

    public CarriageCommand(String command, Byte type) {
        this.command = command;
        this.type = type;
    }

    public static CarriageCommand<Void> getEmptyCommand() {
        return new CarriageCommand<>("", (byte) 0, null);
    }

    public static boolean isEmpty(CarriageCommand<?> carriageCommand) {
        return carriageCommand.getCommand().isEmpty() && carriageCommand.getArgument() == null;
    }
}
