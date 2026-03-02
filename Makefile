.PHONY: build run run-jar clean db-up db-down db-reset

build:
	mvn clean package -DskipTests

run:
	mvn javafx:run

run-jar:
	java -jar target/mall-lease-1.0.0.jar

clean:
	mvn clean

db-up:
	docker compose up -d

db-down:
	docker compose down

db-reset:
	docker compose down -v
	docker compose up -d
