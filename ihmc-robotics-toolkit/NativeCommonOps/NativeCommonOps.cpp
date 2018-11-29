/*
 * NativeCommonOps.cpp
 *
 *  Created on: Nov 27, 2018
 *      Author: Georg Wiedebach
 */

#include <jni.h>
#include <Eigen/Dense>
#include <iostream>
#include "us_ihmc_robotics_linearAlgebra_commonOps_NativeCommonOpsWrapper.h"

using Eigen::MatrixXd;

JNIEXPORT void JNICALL Java_us_ihmc_robotics_linearAlgebra_commonOps_NativeCommonOpsWrapper_computeAB(JNIEnv *env, jobject thisObj,
		jdoubleArray result, jdoubleArray aData, jdoubleArray bData, jint aRows, jint aCols, jint bCols)
{
	jdouble *aDataArray = env->GetDoubleArrayElements(aData, NULL);
	jdouble *bDataArray = env->GetDoubleArrayElements(bData, NULL);
	MatrixXd A = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(aDataArray, aRows, aCols);
	MatrixXd B = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(bDataArray, aCols, bCols);

	MatrixXd AB = A * B;

	jdouble *resultDataArray = new jdouble[aRows * bCols];
	Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(resultDataArray, aRows, bCols) = AB;
	env->SetDoubleArrayRegion(result, 0, aRows * bCols, resultDataArray);

	delete aDataArray;
	delete bDataArray;
	delete resultDataArray;
}

JNIEXPORT void JNICALL Java_us_ihmc_robotics_linearAlgebra_commonOps_NativeCommonOpsWrapper_computeAtBA(JNIEnv *env, jobject thisObj,
		jdoubleArray result, jdoubleArray aData, jdoubleArray bData, jint aRows, jint aCols)
{
	jdouble *aDataArray = env->GetDoubleArrayElements(aData, NULL);
	jdouble *bDataArray = env->GetDoubleArrayElements(bData, NULL);
	MatrixXd A = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(aDataArray, aRows, aCols);
	MatrixXd B = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(bDataArray, aRows, aRows);

	MatrixXd AtBA = A.transpose() * B * A;

	jdouble *resultDataArray = new jdouble[aCols * aCols];
	Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(resultDataArray, aCols, aCols) = AtBA;
	env->SetDoubleArrayRegion(result, 0, aCols * aCols, resultDataArray);

	delete aDataArray;
	delete bDataArray;
	delete resultDataArray;
}

JNIEXPORT void JNICALL Java_us_ihmc_robotics_linearAlgebra_commonOps_NativeCommonOpsWrapper_solve(JNIEnv *env, jobject thisObj,
		jdoubleArray result, jdoubleArray aData, jdoubleArray bData, jint aRows)
{
	jdouble *aDataArray = env->GetDoubleArrayElements(aData, NULL);
	jdouble *bDataArray = env->GetDoubleArrayElements(bData, NULL);
	MatrixXd A = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(aDataArray, aRows, aRows);
	MatrixXd B = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(bDataArray, aRows, 1);

	MatrixXd x = A.lu().solve(B);

	jdouble *resultDataArray = new jdouble[aRows];
	Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(resultDataArray, aRows, 1) = x;
	env->SetDoubleArrayRegion(result, 0, aRows, resultDataArray);

	delete aDataArray;
	delete bDataArray;
	delete resultDataArray;
}

JNIEXPORT void JNICALL Java_us_ihmc_robotics_linearAlgebra_commonOps_NativeCommonOpsWrapper_solveRobust(JNIEnv *env, jobject thisObj,
		jdoubleArray result, jdoubleArray aData, jdoubleArray bData, jint aRows, jint aCols)
{
	jdouble *aDataArray = env->GetDoubleArrayElements(aData, NULL);
	jdouble *bDataArray = env->GetDoubleArrayElements(bData, NULL);
	MatrixXd A = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(aDataArray, aRows, aCols);
	MatrixXd B = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(bDataArray, aRows, 1);

	MatrixXd x = A.householderQr().solve(B);

	jdouble *resultDataArray = new jdouble[aCols];
	Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(resultDataArray, aCols, 1) = x;
	env->SetDoubleArrayRegion(result, 0, aCols, resultDataArray);

	delete aDataArray;
	delete bDataArray;
	delete resultDataArray;
}

JNIEXPORT void JNICALL Java_us_ihmc_robotics_linearAlgebra_commonOps_NativeCommonOpsWrapper_solveDamped(JNIEnv *env, jobject thisObj,
		jdoubleArray result, jdoubleArray aData, jdoubleArray bData, jint aRows, jint aCols, jdouble alpha)
{
	jdouble *aDataArray = env->GetDoubleArrayElements(aData, NULL);
	jdouble *bDataArray = env->GetDoubleArrayElements(bData, NULL);
	MatrixXd A = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(aDataArray, aRows, aCols);
	MatrixXd B = Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(bDataArray, aRows, 1);

	MatrixXd outer = A * A.transpose() + MatrixXd::Identity(aRows, aRows) * alpha * alpha;
	MatrixXd x = A.transpose() * outer.llt().solve(B);

	jdouble *resultDataArray = new jdouble[aCols];
	Eigen::Map<Eigen::Matrix<double, Eigen::Dynamic, Eigen::Dynamic, Eigen::RowMajor>>(resultDataArray, aCols, 1) = x;
	env->SetDoubleArrayRegion(result, 0, aCols, resultDataArray);

	delete aDataArray;
	delete bDataArray;
	delete resultDataArray;
}
