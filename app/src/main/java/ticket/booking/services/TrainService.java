package ticket.booking.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.booking.entities.Train;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainService {

    private List<Train> trainList = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TRAIN_DB_PATH = "app/src/main/java/ticket/booking/localDb/trains.json";

    public TrainService() throws IOException {
        File trainsFile = new File(TRAIN_DB_PATH);
        if (!trainsFile.exists()) {
            trainsFile.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(trainsFile, new ArrayList<Train>());
            trainList = new ArrayList<>();
            return;
        }
        if (trainsFile.length() == 0) {
            trainList = new ArrayList<>();
            return;
        }
        trainList = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
        if (trainList == null) trainList = new ArrayList<>();
    }

    /* ---------- queries ---------- */

    public Train getTrainById(String trainId) {
        if (trainId == null) return null;
        return trainList.stream()
                .filter(t -> trainId.equalsIgnoreCase(t.getTrainId()))
                .findFirst()
                .orElse(null);
    }

    public List<Train> searchTrains(String source, String destination) {
        if (source == null || destination == null) return Collections.emptyList();
        String s = source.toLowerCase(Locale.ROOT);
        String d = destination.toLowerCase(Locale.ROOT);

        return trainList.stream()
                .filter(train -> validTrain(train, s, d))
                .collect(Collectors.toList());
    }

    private boolean validTrain(Train train, String sourceLower, String destLower) {
        List<String> stationOrder = train.getStations();
        if (stationOrder == null || stationOrder.isEmpty()) return false;

        // compare ignoring case by mapping to lower
        List<String> lower = stationOrder.stream()
                .map(x -> x == null ? "" : x.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        int sourceIndex = lower.indexOf(sourceLower);
        int destIndex = lower.indexOf(destLower);

        return sourceIndex != -1 && destIndex != -1 && sourceIndex < destIndex;
    }

    /* ---------- mutations ---------- */

    public void addTrain(Train newTrain) {
        // if train with same id exists, update instead of duplicate
        Train existing = getTrainById(newTrain.getTrainId());
        if (existing != null) {
            updateTrain(newTrain);
            return;
        }
        trainList.add(newTrain);
        saveTrainListToFile();
    }

    public void updateTrain(Train updatedTrain) {
        if (updatedTrain == null || updatedTrain.getTrainId() == null) return;

        OptionalInt idx = IntStream.range(0, trainList.size())
                .filter(i -> updatedTrain.getTrainId().equalsIgnoreCase(trainList.get(i).getTrainId()))
                .findFirst();

        if (idx.isPresent()) {
            trainList.set(idx.getAsInt(), updatedTrain);
        } else {
            trainList.add(updatedTrain);
        }
        saveTrainListToFile();
    }

    private void saveTrainListToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(TRAIN_DB_PATH), trainList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
