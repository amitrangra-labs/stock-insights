# Contributing

Thanks for your interest! This project is early-stage — issues and PRs welcome.

## Architecture rules

The codebase follows a **Domain / Port / Adapter** (hexagonal) layout with **explicit Spring
wiring**. A few rules keep it honest — please respect them in PRs:

1. **No stereotype annotations, no autowiring in production code.** Do not use `@Component`,
   `@Service`, `@Repository`, `@Controller`, or `@Autowired`. Every bean is constructed with a
   plain `new` inside exactly one of the three `@Configuration` classes:
   - `domain/config/DomainConfig` — domain services
   - `adapter/in/config/InboundConfig` — web/REST/scheduler endpoints
   - `adapter/out/config/OutboundConfig` — external clients + persistence, exposed as `port` beans

   Reading those three files should reveal the entire object graph. (`@Autowired` is fine in
   `src/test` — that's test infrastructure, not production wiring.)

   > Note: endpoint classes carry a type-level `@RequestMapping` (and `@GetMapping` etc. on
   > methods). That's routing metadata, not dependency injection — Spring MVC routes any bean
   > whose type has `@RequestMapping`, no `@Controller` needed.

2. **The domain is framework-free.** Classes in `domain/service` and `domain/object` must not
   import Spring, JPA, or any HTTP client. They depend only on plain Java and `port` interfaces.

3. **Ports are outbound-only.** `port` holds the interfaces the domain uses to reach the outside
   world (external APIs, persistence). Inbound adapters call domain services directly — there is
   no inbound port layer.

4. **Adapters depend inward.** `adapter/out/client` classes may depend on `domain/object` types
   and implement `port` interfaces. Nothing in `domain` or `port` may depend on an `adapter`.

5. **Adding a data provider** = a new class in `adapter/out/client` implementing `MarketDataPort`,
   wired in `OutboundConfig`. No changes to the domain or endpoints.

## Before you push

```bash
./mvnw test
```

Keep new code consistent with the surrounding style.
