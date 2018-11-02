package com.thehecklers.cofsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;

@SpringBootApplication
public class CofSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(CofSvcApplication.class, args);
    }
}

@Component
//@RequiredArgsConstructor
class DataLoader {
    private final CoffeeRepo repo;

    DataLoader(CoffeeRepo repo) {
        this.repo = repo;
    }

    @PostConstruct
    private void load() {
        repo.deleteAll().thenMany(
                Flux.just("A", "B", "C", "D")
                        .map(name -> new Coffee(UUID.randomUUID().toString(), name))
                        .flatMap(repo::save))
                .thenMany(repo.findAll())
                .subscribe(System.out::println);

//        repo.deleteAll().thenMany(
//                Flux.interval(Duration.ofSeconds(1))
//                        .map(Object::toString)
//                        .zipWith(Flux.just("A", "B", "C"))
//                        .map(t -> new Coffee(t.getT1(), t.getT2())))
//                .flatMap(repo::save)
//                .thenMany(repo.findAll())
//                .subscribe(System.out::println);
    }
}

@Configuration
//@RequiredArgsConstructor
class RouteConfig {
    private final CoffeeService service;

    RouteConfig(CoffeeService service) {
        this.service = service;
    }

    @Bean
    RouterFunction<?> routerFunction() {
        return route(GET("/coffees"), this::all)
                .andRoute(GET("/coffees/{id}"), this::byId)
                .andRoute(GET("/coffees/{id}/orders"), this::orders);
    }

    private Mono<ServerResponse> all(ServerRequest req) {
        return ServerResponse.ok()
                .body(service.getAllCoffees(), Coffee.class);
    }

    private Mono<ServerResponse> byId(ServerRequest req) {
        return ServerResponse.ok()
                .body(service.getCoffeeById(req.pathVariable("id")), Coffee.class);
    }

    private Mono<ServerResponse> orders(ServerRequest req) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(service.getOrders(req.pathVariable("id")), CoffeeOrder.class);
    }
}

//@RestController
//@RequestMapping("/coffees")
//@RequiredArgsConstructor
//class CoffeeController {
//    private final CoffeeService service;
//
//    @GetMapping
//    Flux<Coffee> all() {
//        return service.getAllCoffees();
//    }
//
//    @GetMapping("/{id}")
//    Mono<Coffee> byId(@PathVariable String id) {
//        return service.getCoffeeById(id);
//    }
//
//    @GetMapping(value = "/{id}/orders", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    Flux<CoffeeOrder> orders(@PathVariable String id) {
//        return service.getOrders(id);
//    }
//}

@Service
//@RequiredArgsConstructor
class CoffeeService {
    private final CoffeeRepo repo;

    CoffeeService(CoffeeRepo repo) {
        this.repo = repo;
    }

    Flux<Coffee> getAllCoffees() {
        return repo.findAll();
    }

    Mono<Coffee> getCoffeeById(String id) {
        return repo.findById(id);
    }

    Flux<CoffeeOrder> getOrders(String coffeeId) {
        return Flux.interval(Duration.ofSeconds(1))
                .onBackpressureDrop()
                .map(i -> new CoffeeOrder(coffeeId, Instant.now()));
    }
}

interface CoffeeRepo extends ReactiveCrudRepository<Coffee, String> {
}

//@Value
class CoffeeOrder {
    private final String coffeeId;
    private final Instant now;

    public CoffeeOrder(String coffeeId, Instant now) {
        this.coffeeId = coffeeId;
        this.now = now;
    }

    public String getCoffeeId() {
        return coffeeId;
    }

    public Instant getNow() {
        return now;
    }

    @Override
    public String toString() {
        return "CoffeeOrder{" +
                "coffeeId='" + coffeeId + '\'' +
                ", now=" + now +
                '}';
    }
}

//@Value
@Document
class Coffee {
    private final String id;
    private final String name;

    public Coffee(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Coffee{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}