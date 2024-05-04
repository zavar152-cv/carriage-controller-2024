package ru.itmo.zavar.carriagecontroller.ui.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.itmo.zavar.carriagecontroller.carriage.actions.CarriageAction;

import java.util.HashMap;
import java.util.LinkedList;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public final class MapData {
    private HashMap<String, CarriagePoint> carriagePoints;
    private double minXCoordinate, maxXCoordinate, minYCoordinate, maxYCoordinate;
    private LinkedList<CarriageAction<?>> actions;
}
