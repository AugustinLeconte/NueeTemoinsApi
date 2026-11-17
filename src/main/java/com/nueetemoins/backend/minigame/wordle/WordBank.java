package com.nueetemoins.backend.minigame.wordle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Dictionnaires par langue ET par longueur, dans src/main/resources/words/ :
 *   {lang}-{len}-answers.txt : mots pouvant être la solution (1 mot / ligne)
 *   {lang}-{len}-valid.txt   : (optionnel) grand dictionnaire de mots acceptés en proposition
 * Ex. : fr-5-answers.txt, fr-6-answers.txt, fr-7-answers.txt, en-5-answers.txt...
 * Les accents sont retirés : "ÉGLISE" -> "EGLISE".
 *
 * La longueur du mot du jour cycle sur LENGTHS (5, 6, 7, 5, 6, 7...), identique pour tous les joueurs.
 * ATTENTION : ne pas modifier un fichier -answers en prod (le mot du jour changerait).
 */
@Component
public class WordBank {

    public static final List<Integer> LENGTHS = List.of(5, 6, 7);
    private static final String DEFAULT_LANG = "en";
    private static final List<String> LANGS = List.of("fr", "en", "es");

    private final boolean strict;
    private final Set<String> supported = new HashSet<>();
    private final Map<String, List<String>> answers = new HashMap<>(); // clé "fr:5"
    private final Map<String, Set<String>> valid = new HashMap<>();

    /**
     * strict=false (défaut) : toute suite de lettres de la bonne longueur est acceptée.
     * strict=true : seuls les mots des fichiers answers/valid sont acceptés
     * (à activer quand tu auras ajouté de gros fichiers -valid.txt).
     */
    public WordBank(@Value("${minigames.wordle.seed:20240601}") long seed,
                    @Value("${minigames.wordle.strict-dictionary:false}") boolean strict) {
        this.strict = strict;
        for (String lang : LANGS) {
            boolean complete = true;
            for (int len : LENGTHS) {
                List<String> a = new ArrayList<>(load(file(lang, len, "answers"), len));
                if (a.isEmpty()) { complete = false; continue; }
                Collections.sort(a); // base stable, indépendante de l'ordre du fichier
                Collections.shuffle(a, new Random(seed + lang.hashCode() * 31L + len));
                Set<String> v = new HashSet<>(a);
                v.addAll(load(file(lang, len, "valid"), len));
                answers.put(key(lang, len), a);
                valid.put(key(lang, len), v);
            }
            if (complete) supported.add(lang);
        }
        if (!supported.contains(DEFAULT_LANG)) {
            throw new IllegalStateException("words/en-{5,6,7}-answers.txt manquants (langue de repli)");
        }
    }

    /** "fr-FR" / "FR" / null -> langue disponible pour toutes les longueurs, sinon repli sur l'anglais. */
    public String resolveLang(String lang) {
        if (lang == null || lang.length() < 2) return DEFAULT_LANG;
        String l = lang.substring(0, 2).toLowerCase(Locale.ROOT);
        return supported.contains(l) ? l : DEFAULT_LANG;
    }

    /** Longueur du mot du jour : identique pour tous les joueurs. */
    public int lengthForDay(LocalDate date) {
        return LENGTHS.get((int) Math.floorMod(date.toEpochDay(), (long) LENGTHS.size()));
    }

    /** Même mot pour tous les joueurs d'une langue un jour donné, sans répétition avant la fin du cycle. */
    public String dailyWord(String lang, int length, LocalDate date) {
        List<String> list = answers.get(key(lang, length));
        long cycle = date.toEpochDay() / LENGTHS.size(); // 1 mot par "tour" de longueurs
        return list.get((int) Math.floorMod(cycle, (long) list.size()));
    }

    public boolean isValid(String lang, int length, String normalizedWord) {
        if (normalizedWord.length() != length) return false;
        if (!strict) return normalizedWord.matches("[A-Z]+");
        return valid.getOrDefault(key(lang, length), Set.of()).contains(normalizedWord);
    }

    public static String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String key(String lang, int len) { return lang + ":" + len; }

    private static String file(String lang, int len, String kind) {
        return "words/" + lang + "-" + len + "-" + kind + ".txt";
    }

    private static List<String> load(String path, int len) {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) return List.of();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            Set<String> out = new LinkedHashSet<>();
            String line;
            while ((line = r.readLine()) != null) {
                String w = normalize(line);
                if (w.matches("[A-Z]{" + len + "}")) out.add(w);
            }
            return new ArrayList<>(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}