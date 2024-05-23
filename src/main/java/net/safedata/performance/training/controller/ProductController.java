package net.safedata.performance.training.controller;

import net.safedata.performance.training.model.Product;
import net.safedata.performance.training.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(final ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/long/{productType}")
    public List<Product> longRunningOperation(@PathVariable final String productType) {
        return productService.getALotOfProducts(productType, "non-synchronized");
    }

    @GetMapping
    public List<net.safedata.performance.training.domain.model.Product> allProducts() {
        return productService.getDatabaseProducts();
    }

    @GetMapping("/long/sync/{productType}")
    public List<Product> getSynchronizedProducts(@PathVariable final String productType) {
        return productService.getSynchronizedProducts(productType);
    }

    @GetMapping("/deferred-result")
    public DeferredResult<ResponseEntity<?>> deferredResultProcessing() {
        DeferredResult<ResponseEntity<?>> deferredResult = new DeferredResult<>();
        deferredResult.onTimeout(() -> ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                                                     .body("The request has timed-out"));
        try {
            //setTotalSalesSync(deferredResult);
            setTotalSalesAsync(deferredResult);
            //if (true) throw new RuntimeException("Ooops :)");
        } catch (RuntimeException ex) {
            deferredResult.setErrorResult(ResponseEntity.badRequest()
                                                        .body(ex.getMessage()));
        }

        return deferredResult;
    }

    @GetMapping("/cf")
    public CompletableFuture<ResponseEntity<?>> completableFeature() {
        return CompletableFuture.completedFuture(ResponseEntity.ok()
                                                               .body(productService.getTotalSales()));
    }

    private void setTotalSalesSync(DeferredResult<ResponseEntity<?>> deferredResult) {
        final double totalSales = productService.getTotalSales();
        deferredResult.setResult(ResponseEntity.ok().body("The total sales value is " + totalSales));
    }

    private void setTotalSalesAsync(DeferredResult<ResponseEntity<?>> deferredResult) {
        CompletableFuture.supplyAsync(productService::getTotalSales)
                         .thenAcceptAsync(value -> deferredResult.setResult(ResponseEntity.ok()
                                                                                          .body("The total sales value is " + value)));
    }

    @GetMapping("/pool-size")
    public String getPoolSize() {
        final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        return forkJoinPool.getPoolSize() + " / " + Runtime.getRuntime().availableProcessors();
    }
}