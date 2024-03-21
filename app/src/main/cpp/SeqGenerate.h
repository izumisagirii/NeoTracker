//
// Created by CUI on 2/26/24.
//

#ifndef A1P_SEQGENERATE_H
#define A1P_SEQGENERATE_H

#include "GlobalData.h"
#include <cmath>
#include <cassert>
#include <string>
#include "json.h"
#include <android/log.h>

class Complex {
public:
    Complex(double real, double imag) : real(real), imag(imag) {}

    Complex() : real(0), imag(0) {}

    Complex operator+(const Complex &other) const {
        return Complex(this->real + other.real, this->imag + other.imag);
    }

    Complex operator-(const Complex &other) const {
        return Complex(this->real - other.real, this->imag - other.imag);
    }

    Complex operator*(const Complex &other) const {
        double realPart = this->real * other.real - this->imag * other.imag;
        double imagPart = this->real * other.imag + this->imag * other.real;
        return Complex(realPart, imagPart);
    }

    Complex operator*(double scalar) const {
        return Complex(this->real * scalar, this->imag * scalar);
    }

    Complex &operator=(const Complex &other) {
        if (this != &other) {
            this->real = other.real;
            this->imag = other.imag;
        }
        return *this;
    }

    std::string toString() const {
        return "{" + std::to_string(real) + "," + std::to_string(imag) + "}";
    }

    friend std::ostream& operator<<(std::ostream& os, const Complex& c) {
        os << "{" << c.real << "," << c.imag << "}";
        return os;
    }

    double magnitude() const {
        return std::sqrt(real * real + imag * imag);
    }
    double phase() const {
        return std::atan2(imag, real);
    }

    double real;
    double imag;
};



class SeqGenerate {
public:
    SeqGenerate(int fc);

    void init();

    double getNew();

    Complex deModNew(double input);
    Complex filteredNew(Complex &input);

    int16_t getNewInt16();

    bool isInit;
private:
    int carryRate;
    std::array<Complex,ZC_LENGTH> array;
    std::array<Complex,48000> multiplyArray;
    std::array<Complex,ZC_LENGTH>::iterator pArray;
    std::array<Complex,48000>::iterator pCarry;
    std::array<Complex,48000>::iterator pDeMod;
    std::deque<Complex> window;
    std::deque<Complex> window_cor;

    std::string readJsonFromAssets(std::string fileName);

    std::array<Complex, ZC_LENGTH> jsonToComplexArray(const std::string &json);
    Complex correlation(Complex &input);

    void generateCarrierArray(int fc);
};


#endif //A1P_SEQGENERATE_H
