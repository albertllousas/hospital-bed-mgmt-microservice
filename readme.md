# Hospital bed mgmt microservice

Keywords: `Functional-Core-Imperative-Shell`, `microservice`, `kotlin`, `micronaut`, `DEbezium`, `SOLID`, `Domain-Driven Design`, `functional-programming`,
`Event-Driven Architecture`, `Domain-Events`, `Kafka`, `MongoDB`, `Transactional-outbox`, `optimistic-locking`

## Problem

A bed hospital management system, using [functional-core, imperative shell](https://medium.com/@albert.llousas/building-modern-architectures-functional-core-imperative-shell-revamp-0bb5ae62b589) code architectural pattern, kotlin and micronaut.

## Functional requirements

- The system must allow the allocation of an already admitted patient to an available bed.
- The system shall prevent the allocation of a bed that is already occupied.
- The system must list all available beds for allocation
- The system must allow staff to add new beds to the system
- The system shall allow the release of a bed when a patient is discharged
- The system shall facilitate the transfer of a patient from one bed to another within the hospital
