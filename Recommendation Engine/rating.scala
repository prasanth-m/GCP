// Databricks notebook source
dbutils.fs.ls ("dbfs:/FileStore/tables/")

// COMMAND ----------

dbutils.fs.cp("dbfs:/FileStore/tables/", "dbfs:/tmp/",recurse=true)

// COMMAND ----------

import org.apache.spark.sql.types._
import org.apache.spark.sql.functions.col
val rating_schema = new StructType()
      .add("MOVIEID", StringType, true)
      .add("RATING", StringType, true)
      .add("TIMESTAMP", StringType, true)
      .add("USERID", StringType, true)

// COMMAND ----------

val temp=spark.read.schema(rating_schema).json("dbfs:/tmp/ratings_data_019d2243_0000_1534_0000_00010cbef1ed_0_0_0_json.gz").persist()

//val ratings = temp.withColumn("USERID",col("USERID").cast(IntegerType))
temp.printSchema()

// COMMAND ----------

import org.apache.spark.sql.DataFrame
// casting of all columns with idiomatic approach in scala
def castAllTypedColumnsTo(df: DataFrame, sourceType: DataType, targetType: DataType) = {
  df.schema.filter(_.dataType == sourceType).foldLeft(df) {
    case (acc, col) => acc.withColumn(col.name, df(col.name).cast(targetType))
  }
}

// COMMAND ----------

val ratings = castAllTypedColumnsTo(temp,StringType,IntegerType)
ratings.show()

// COMMAND ----------

val Array(training, test) = ratings.randomSplit(Array(0.8, 0.2))

// COMMAND ----------

// Build the recommendation model using ALS on the training data
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.ALS

val als = new ALS().setMaxIter(5).setRegParam(0.01).setUserCol("USERID")      
  .setItemCol("MOVIEID")
  .setRatingCol("RATING")
val model = als.fit(training)

// COMMAND ----------

model.setColdStartStrategy("drop")
val predictions = model.transform(test)

val evaluator = new RegressionEvaluator()
  .setMetricName("rmse")
  .setLabelCol("RATING")
  .setPredictionCol("prediction")
val rmse = evaluator.evaluate(predictions)
println(s"Root-mean-square error = $rmse")

// COMMAND ----------

// Generate top 10 movie recommendations for each user
val userRecs = model.recommendForAllUsers(10)
//val userSubsetRecs = model.recommendForUserSubset(userRecs, 10)

// Generate top 10 user recommendations for each movie
val movieRecs = model.recommendForAllItems(10)
//val movieSubSetRecs  = model.recommendForUserSubset(movieRecs, 10)
userRecs.show(10)
