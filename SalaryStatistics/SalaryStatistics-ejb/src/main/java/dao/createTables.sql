CREATE TABLE "Sector"
(
"id" INT not null primary key
    GENERATED ALWAYS AS IDENTITY
    (START WITH 1, INCREMENT BY 1),
"code" VARCHAR(1) NOT NULL,
"name" VARCHAR(40) NOT NULL,
"country" VARCHAR(20) NOT NULL,
"year" VARCHAR(10) NOT NULL,
"averageSalary" DOUBLE NOT NULL
);

CREATE TABLE "Age"
(
"id" INT not null primary key
    GENERATED ALWAYS AS IDENTITY
    (START WITH 1, INCREMENT BY 1),
"ageFrom" INT NOT NULL,
"ageTo" INT NOT NULL,
"country" VARCHAR(20) NOT NULL,
"year" VARCHAR(10) NOT NULL,
"sex" VARCHAR(10),
"averageSalary" DOUBLE NOT NULL
);

CREATE TABLE "Education"
(
"id" INT not null primary key
    GENERATED ALWAYS AS IDENTITY
    (START WITH 1, INCREMENT BY 1),
"degree" VARCHAR(40) NOT NULL,
"country" VARCHAR(20) NOT NULL,
"year" VARCHAR(10) NOT NULL,
"sex" VARCHAR(10),
"averageSalary" DOUBLE NOT NULL
);

CREATE TABLE "Region"
(
"id" INT not null primary key
    GENERATED ALWAYS AS IDENTITY
    (START WITH 1, INCREMENT BY 1),
"name" VARCHAR(40) NOT NULL,
"country" VARCHAR(20) NOT NULL,
"year" VARCHAR(10) NOT NULL,
"sex" VARCHAR(10),
"averageSalary" DOUBLE NOT NULL
);

CREATE TABLE "Classification"
(
"id" INT not null primary key
    GENERATED ALWAYS AS IDENTITY
    (START WITH 1, INCREMENT BY 1),
"code" VARCHAR(2) NOT NULL,
"name" VARCHAR(40) NOT NULL,
"country" VARCHAR(20) NOT NULL,
"year" VARCHAR(10) NOT NULL,
"averageSalary" DOUBLE NOT NULL
);