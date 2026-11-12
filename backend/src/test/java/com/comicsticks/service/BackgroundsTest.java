package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.comicsticks.dto.Background;
import com.comicsticks.dto.PlaceDto;

/** The demo picks a place's background from the words the story uses for it. */
class BackgroundsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            She made tea in the kitchen.          | KITCHEN
            He went up to his bedroom.            | BEDROOM
            They fed the hens at the farm.        | FARM
            The swings creaked.                   | PLAYGROUND
            They skated on the pond.              | LAKE
            They climbed the mountain.            | MOUNTAIN
            Snow fell all night.                  | SNOW
            They waited for the train.            | STATION
            She found a book in the library.      | LIBRARY
            They walked down the street.          | STREET
            The mine was dark.                    | TUNNEL
            """)
    void findsTheNewPlaces(String text, Background background) {
        assertThat(DemoCharacterFinder.placesIn(text)).extracting(PlaceDto::background).contains(background);
    }
}
