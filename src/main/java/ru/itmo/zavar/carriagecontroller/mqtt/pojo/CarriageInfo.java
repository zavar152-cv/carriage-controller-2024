package ru.itmo.zavar.carriagecontroller.mqtt.pojo;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public final class CarriageInfo {
    private Boolean direction;
    private Float targetSpeed;
    private Float targetPosition;
    private Float currentASpeed;
    private Float currentBSpeed;
    private Float currentPosition;
    private Byte currentStatus;
    private Byte externalModuleStatus;
}
