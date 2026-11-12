package com.comicsticks.dto;

/**
 * A main character's fixed look, from the character sheet. Every panel draws the character from
 * this, so they look the same all through the book.
 *
 * @param name        the name used in every panel
 * @param description one plain line, such as "Mole: a shy, kind animal who lives underground"
 * @param kind        person or animal
 * @param color       the figure's ink colour, as a CSS hex colour
 * @param hat         identifying features: one or two of hat, hair and accessory are set
 * @param ears        animal parts, set only for animals
 * @param added       true (never null) when the character was not on the reviewed sheet but appeared in a panel,
 *                    so it got a plain default design that the user may want to check
 * @param species     what kind of animal, or {@code none}; always {@code none} for a person
 * @param stance      upright like a person, or on four legs like a real animal; always upright for a person
 */
public record CharacterDesign(
        String name,
        String description,
        CharacterKind kind,
        String color,
        Hat hat,
        Hair hair,
        Accessory accessory,
        Ears ears,
        AnimalTail tail,
        Snout snout,
        Boolean added,
        Species species,
        Stance stance) {

    public CharacterDesign {
        added = Boolean.TRUE.equals(added);
    }

    /** A design with no species, standing upright unless normalized otherwise. */
    public CharacterDesign(String name, String description, CharacterKind kind, String color, Hat hat, Hair hair,
            Accessory accessory, Ears ears, AnimalTail tail, Snout snout, Boolean added) {
        this(name, description, kind, color, hat, hair, accessory, ears, tail, snout, added, null, null);
    }
}
