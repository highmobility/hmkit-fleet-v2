package com.highmobility.hmkitfleet

import okhttp3.OkHttpClient

class HMKitConfiguration private constructor(builder: Builder) {
  val client: OkHttpClient

  init {
    client = builder.client ?: OkHttpClient()
  }

  class Builder {
    var client: OkHttpClient? = null
      private set

    fun client(client: OkHttpClient) = apply { this.client = client }
    fun build() = HMKitConfiguration(this)
  }

  companion object {
    fun defaultConfiguration(): HMKitConfiguration {
      return Builder().build()
    }
  }
}
