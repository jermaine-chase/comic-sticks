package com.comicsticks.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An object or a background animal that can be drawn in a panel. Each one lists the words that name
 * it in a story: a thing is only drawn when the panel's text uses one of them, so nothing appears
 * that the story does not mention. Plurals ending in "s" or "es" match on their own; list any other
 * plural. Keep in sync with THINGS in comic.models.ts.
 */
public enum Thing {
    // Objects small enough to hold.
    @JsonProperty("key") KEY(Kind.HELD, "key"),
    @JsonProperty("map") MAP(Kind.HELD, "map"),
    @JsonProperty("book") BOOK(Kind.HELD, "book", "diary", "notebook"),
    @JsonProperty("letter") LETTER(Kind.HELD, "letter", "envelope", "postcard"),
    @JsonProperty("bag") BAG(Kind.HELD, "bag", "backpack", "rucksack", "satchel", "sack", "purse"),
    @JsonProperty("box") BOX(Kind.HELD, "box", "treasure chest", "crate"),
    @JsonProperty("ball") BALL(Kind.HELD, "ball", "football"),
    @JsonProperty("cup") CUP(Kind.HELD, "cup", "mug", "glass of", "teacup"),
    @JsonProperty("bottle") BOTTLE(Kind.HELD, "bottle", "jar"),
    @JsonProperty("food") FOOD(Kind.HELD, "food", "sandwich", "bread", "cheese", "pie", "biscuit",
            "cookie", "picnic", "lunch", "supper", "dinner", "breakfast"),
    @JsonProperty("apple") APPLE(Kind.HELD, "apple"),
    @JsonProperty("cake") CAKE(Kind.HELD, "cake"),
    @JsonProperty("lamp") LAMP(Kind.HELD, "lamp", "lantern"),
    @JsonProperty("candle") CANDLE(Kind.HELD, "candle"),
    @JsonProperty("torch") TORCH(Kind.HELD, "torch", "flashlight"),
    @JsonProperty("umbrella") UMBRELLA(Kind.HELD, "umbrella"),
    @JsonProperty("phone") PHONE(Kind.HELD, "phone", "telephone"),
    @JsonProperty("sword") SWORD(Kind.HELD, "sword", "dagger"),
    @JsonProperty("shield") SHIELD(Kind.HELD, "shield"),
    @JsonProperty("crown") CROWN(Kind.HELD, "crown"),
    @JsonProperty("rope") ROPE(Kind.HELD, "rope"),
    @JsonProperty("bucket") BUCKET(Kind.HELD, "bucket", "pail"),
    @JsonProperty("basket") BASKET(Kind.HELD, "basket"),
    @JsonProperty("coin") COIN(Kind.HELD, "coin", "penny", "pennies", "money"),
    @JsonProperty("picture") PICTURE(Kind.HELD, "picture", "photo", "photograph", "painting", "portrait"),
    @JsonProperty("clock") CLOCK(Kind.HELD, "clock", "pocket watch"),
    @JsonProperty("stick") STICK(Kind.HELD, "stick", "wand", "branch"),
    @JsonProperty("rock") ROCK(Kind.HELD, "rock", "stone", "pebble"),
    @JsonProperty("gift") GIFT(Kind.HELD, "gift", "parcel", "package"),
    @JsonProperty("flower") FLOWER(Kind.HELD, "flower", "rose", "daisy", "daisies", "tulip", "bouquet"),
    // Large objects, drawn as scenery and never held.
    @JsonProperty("tree") TREE(Kind.LARGE, "tree"),
    @JsonProperty("door") DOOR(Kind.LARGE, "door", "gate"),
    @JsonProperty("chair") CHAIR(Kind.LARGE, "chair", "stool", "armchair"),
    @JsonProperty("table") TABLE(Kind.LARGE, "table", "desk"),
    @JsonProperty("bed") BED(Kind.LARGE, "bed"),
    @JsonProperty("ladder") LADDER(Kind.LARGE, "ladder"),
    // Animals that are not characters, drawn small as part of the scene.
    @JsonProperty("dog") DOG(Kind.ANIMAL, "dog", "puppy", "puppies", "hound"),
    @JsonProperty("cat") CAT(Kind.ANIMAL, "cat", "kitten"),
    @JsonProperty("bird") BIRD(Kind.ANIMAL, "bird", "sparrow", "robin", "crow", "seagull", "gull"),
    @JsonProperty("owl") OWL(Kind.ANIMAL, "owl"),
    @JsonProperty("duck") DUCK(Kind.ANIMAL, "duck", "goose", "geese", "swan"),
    @JsonProperty("chicken") CHICKEN(Kind.ANIMAL, "chicken", "hen", "rooster", "chick"),
    @JsonProperty("horse") HORSE(Kind.ANIMAL, "horse", "pony", "ponies"),
    @JsonProperty("cow") COW(Kind.ANIMAL, "cow", "bull", "calf", "calves", "cattle"),
    @JsonProperty("sheep") SHEEP(Kind.ANIMAL, "sheep", "lamb"),
    @JsonProperty("pig") PIG(Kind.ANIMAL, "pig", "piglet"),
    @JsonProperty("rabbit") RABBIT(Kind.ANIMAL, "rabbit", "bunny", "bunnies", "hare"),
    @JsonProperty("mouse") MOUSE(Kind.ANIMAL, "mouse", "mice"),
    @JsonProperty("fox") FOX(Kind.ANIMAL, "fox"),
    @JsonProperty("bear") BEAR(Kind.ANIMAL, "bear"),
    @JsonProperty("frog") FROG(Kind.ANIMAL, "frog", "toad"),
    @JsonProperty("fish") FISH(Kind.ANIMAL, "fish"),
    @JsonProperty("snake") SNAKE(Kind.ANIMAL, "snake"),
    @JsonProperty("butterfly") BUTTERFLY(Kind.ANIMAL, "butterfly", "butterflies");

    /** How a thing is drawn: in a hand, as scenery, or as a small animal in the scene. */
    public enum Kind {
        HELD, LARGE, ANIMAL
    }

    private final Kind kind;
    private final List<String> words;

    Thing(Kind kind, String... words) {
        this.kind = kind;
        this.words = List.of(words);
    }

    public Kind kind() {
        return kind;
    }

    /** Only small objects can be in a character's hand. */
    public boolean canBeHeld() {
        return kind == Kind.HELD;
    }

    /** The words that name this thing in a story, in lowercase. */
    public List<String> words() {
        return words;
    }
}
