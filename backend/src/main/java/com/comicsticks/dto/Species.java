package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What kind of animal a character is. Each species has the ears, tail, snout and stance it is drawn
 * with by default, and the frontend gives it its own colouring and one telling mark (a mane, wool,
 * spines, wings...), so the reader can tell a cat from a mouse at a glance. {@code none} is a person,
 * or an animal that is none of these. Keep in sync with SPECIES in comic.models.ts.
 */
public enum Species {
    @JsonEnumDefaultValue @JsonProperty("none") NONE(Ears.NONE, AnimalTail.NONE, Snout.NONE, Stance.UPRIGHT),
    @JsonProperty("cat") CAT(Ears.POINTY, AnimalTail.LONG, Snout.SHORT, Stance.FOUR_LEGS),
    @JsonProperty("dog") DOG(Ears.FLOPPY, AnimalTail.SHORT, Snout.SHORT, Stance.FOUR_LEGS),
    @JsonProperty("wolf") WOLF(Ears.POINTY, AnimalTail.BUSHY, Snout.LONG, Stance.FOUR_LEGS),
    @JsonProperty("fox") FOX(Ears.POINTY, AnimalTail.BUSHY, Snout.LONG, Stance.FOUR_LEGS),
    @JsonProperty("rabbit") RABBIT(Ears.LONG, AnimalTail.SHORT, Snout.SHORT, Stance.UPRIGHT),
    @JsonProperty("mouse") MOUSE(Ears.ROUND, AnimalTail.LONG, Snout.SHORT, Stance.UPRIGHT),
    @JsonProperty("rat") RAT(Ears.ROUND, AnimalTail.LONG, Snout.LONG, Stance.UPRIGHT),
    @JsonProperty("mole") MOLE(Ears.NONE, AnimalTail.SHORT, Snout.LONG, Stance.UPRIGHT),
    @JsonProperty("badger") BADGER(Ears.ROUND, AnimalTail.SHORT, Snout.LONG, Stance.UPRIGHT),
    @JsonProperty("squirrel") SQUIRREL(Ears.POINTY, AnimalTail.BUSHY, Snout.SHORT, Stance.UPRIGHT),
    @JsonProperty("hedgehog") HEDGEHOG(Ears.ROUND, AnimalTail.NONE, Snout.LONG, Stance.UPRIGHT),
    @JsonProperty("bear") BEAR(Ears.ROUND, AnimalTail.SHORT, Snout.SHORT, Stance.FOUR_LEGS),
    @JsonProperty("lion") LION(Ears.ROUND, AnimalTail.LONG, Snout.SHORT, Stance.FOUR_LEGS),
    @JsonProperty("pig") PIG(Ears.FLOPPY, AnimalTail.CURLY, Snout.SHORT, Stance.FOUR_LEGS),
    @JsonProperty("horse") HORSE(Ears.POINTY, AnimalTail.LONG, Snout.LONG, Stance.FOUR_LEGS),
    @JsonProperty("cow") COW(Ears.FLOPPY, AnimalTail.LONG, Snout.SHORT, Stance.FOUR_LEGS),
    @JsonProperty("sheep") SHEEP(Ears.FLOPPY, AnimalTail.SHORT, Snout.SHORT, Stance.FOUR_LEGS),
    @JsonProperty("frog") FROG(Ears.NONE, AnimalTail.NONE, Snout.NONE, Stance.UPRIGHT),
    @JsonProperty("toad") TOAD(Ears.NONE, AnimalTail.NONE, Snout.NONE, Stance.UPRIGHT),
    @JsonProperty("bird") BIRD(Ears.NONE, AnimalTail.SHORT, Snout.BEAK, Stance.UPRIGHT),
    @JsonProperty("owl") OWL(Ears.POINTY, AnimalTail.SHORT, Snout.BEAK, Stance.UPRIGHT),
    @JsonProperty("duck") DUCK(Ears.NONE, AnimalTail.SHORT, Snout.BEAK, Stance.UPRIGHT);

    private final Ears ears;
    private final AnimalTail tail;
    private final Snout snout;
    private final Stance stance;

    Species(Ears ears, AnimalTail tail, Snout snout, Stance stance) {
        this.ears = ears;
        this.tail = tail;
        this.snout = snout;
        this.stance = stance;
    }

    public Ears ears() {
        return ears;
    }

    public AnimalTail tail() {
        return tail;
    }

    public Snout snout() {
        return snout;
    }

    /** How the species is drawn unless the sheet says otherwise. */
    public Stance stance() {
        return stance;
    }

    /** The species named by a word such as "kitten" or "Rat", or NONE. */
    public static Species fromWord(String word) {
        String w = word == null ? "" : word.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (w) {
            case "kitten", "kitty", "cats" -> CAT;
            case "puppy", "hound", "dogs" -> DOG;
            case "bunny", "hare" -> RABBIT;
            case "mice" -> MOUSE;
            case "ratty", "rats" -> RAT;
            case "piglet", "hog", "boar" -> PIG;
            case "pony", "foal" -> HORSE;
            case "bull", "calf" -> COW;
            case "lamb", "ram" -> SHEEP;
            case "lioness", "cub" -> LION;
            case "sparrow", "robin", "crow", "blackbird", "parrot" -> BIRD;
            case "goose", "swan", "duckling" -> DUCK;
            default -> java.util.Arrays.stream(values())
                    .filter(s -> s != NONE && s.name().equalsIgnoreCase(w))
                    .findFirst()
                    .orElse(NONE);
        };
    }
}
