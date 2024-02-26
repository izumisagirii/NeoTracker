//
// Created by CUI on 2/26/24.
//

#ifndef A1P_SEQGENERATE_H
#define A1P_SEQGENERATE_H

#include "GlobalData.h"
#include <cmath>
#include <string>
#include <vector>
#include "json/json.h"

class Complex {
public:
    Complex(double real, double imag) {
        this->real = real;
        this->imag = imag;
    }

    Complex() {
        this->real = 0;
        this->imag = 0;
    }

    Complex operator*(const Complex &other) {
        double realPart = this->real * other.real - this->imag * other.imag;
        double imagPart = this->real * other.imag + this->imag * other.real;
        return Complex(realPart, imagPart);
    }

    Complex &operator=(const Complex &other) {
        if (this == &other) {
            return *this;
        }
        this->real = other.real;
        this->imag = other.imag;
        return *this;
    }

    double real;
    double imag;
};


class SeqGenerate {
public:
    SeqGenerate(int fc);
    void init();

    double getNew();

    int16_t getNewInt16();
    bool isInit;
private:
    int carryRate;
    std::vector<Complex> array;
    std::vector<Complex> multiplyArray;
    int pArray;
    int pCarry;
    int boundArray;

    std::string readJsonFromAssets(std::string fileName);

    std::vector<Complex> jsonToComplexArray(std::string json);

    void generateCarrierArray(int fc);
};


#endif //A1P_SEQGENERATE_H
