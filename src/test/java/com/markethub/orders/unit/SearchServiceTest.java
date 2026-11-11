package com.markethub.orders.unit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.markethub.orders.application.SearchService;
import com.markethub.orders.application.SearchView;
import com.markethub.orders.domain.exception.BadRequestException;
import com.markethub.orders.domain.model.ProductSearchQuery;
import com.markethub.orders.domain.model.ProductSearchResult;
import com.markethub.orders.domain.model.SearchSort;
import com.markethub.orders.infrastructure.search.ProductSearchRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock ProductSearchRepository repository;
    @InjectMocks SearchService service;

    @BeforeEach
    void stub() {
        lenient().when(repository.search(any())).thenReturn(new ProductSearchResult(List.of(), 41, List.of(), List.of()));
    }

    private ProductSearchQuery sentQuery() {
        ArgumentCaptor<ProductSearchQuery> captor = ArgumentCaptor.forClass(ProductSearchQuery.class);
        verify(repository).search(captor.capture());
        return captor.getValue();
    }

    @ParameterizedTest(name = "size {0} becomes {1}")
    @CsvSource({ "500, 100", "101, 100", "100, 100", "20, 20", "1, 1", "0, 1", "-5, 1" })
    void search_shouldKeepPageSizeBetweenOneAndOneHundred(int requested, int expected) {
        service.search(null, null, null, null, null, null, 0, requested);

        assertThat(sentQuery().size()).isEqualTo(expected);
    }

    @Test
    void search_shouldUseDefaults_whenNothingIsGiven() {
        SearchView view = service.search(null, null, null, null, null, null, null, null);

        ProductSearchQuery query = sentQuery();
        assertAll(
            () -> assertThat(query.page()).isZero(),
            () -> assertThat(query.size()).isEqualTo(20),
            () -> assertThat(query.sort()).isEqualTo(SearchSort.RELEVANCE),
            () -> assertThat(query.text()).isNull(),
            () -> assertThat(query.brands()).isEmpty(),
            () -> assertThat(view.totalPages()).isEqualTo(3));          // 41 results in pages of 20
    }

    @Test
    void search_shouldTreatBlankTextAsNoText_andFixNegativePage() {
        service.search("   ", null, null, null, null, "price_desc", -4, 10);

        ProductSearchQuery query = sentQuery();
        assertAll(
            () -> assertThat(query.text()).isNull(),
            () -> assertThat(query.page()).isZero(),
            () -> assertThat(query.sort()).isEqualTo(SearchSort.PRICE_DESC));
    }

    @Test
    void search_shouldRejectAnUnknownSort_beforeTouchingElasticsearch() {
        assertThatThrownBy(() -> service.search(null, null, null, null, null, "popularity", 0, 20))
            .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }
}
