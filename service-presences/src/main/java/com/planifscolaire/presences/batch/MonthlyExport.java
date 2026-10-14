package com.planifscolaire.presences.batch;

import com.planifscolaire.presences.repository.AppelRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.TreeMap;

@Component
@ConditionalOnProperty(name = "app.export.enabled", havingValue = "true", matchIfMissing = true)
public class MonthlyExport {
    private final AppelRepository appels;
    private final Path directory;

    public MonthlyExport(
            AppelRepository appels, @Value("${app.export.directory:./exports}") String directory) {
        this.appels = appels;
        this.directory = Path.of(directory);
    }

    @Scheduled(cron = "${app.export.cron:0 0 2 1 * *}", zone = "Africa/Casablanca")
    public void run() throws IOException {
        export(YearMonth.now(ZoneId.of("Africa/Casablanca")).minusMonths(1));
    }

    public Path export(YearMonth month) throws IOException {
        var totals = new TreeMap<Long, Integer>();
        appels.findByDateGreaterThanEqualAndDateLessThan(
                        month.atDay(1), month.plusMonths(1).atDay(1))
                .forEach(a -> totals.merge(a.getClasseId(), a.getNbAbsences(), Integer::sum));
        StringBuilder csv = new StringBuilder("mois,classeId,nbAbsences\n");
        totals.forEach(
                (classe, total) ->
                        csv.append(month)
                                .append(',')
                                .append(classe)
                                .append(',')
                                .append(total)
                                .append('\n'));
        Files.createDirectories(directory);
        Path target = directory.resolve("absences-" + month + ".csv");
        Path temp = Files.createTempFile(directory, "absences-", ".tmp");
        try {
            Files.writeString(temp, csv, StandardCharsets.UTF_8);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        return target;
    }
}
