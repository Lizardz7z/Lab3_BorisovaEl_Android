package com.example.borisovael_lab3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.borisovael_lab3.BuildConfig
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


class MainActivity : AppCompatActivity() {

    private lateinit var items: List<NewsResponse.Article>
    private lateinit var adapter: NewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        items = listOf()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NewsAdapter(items)
        recyclerView.adapter = adapter

        fetchData()

        val searchView = findViewById<SearchView>(R.id.searchnews)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                var filteredNews =
                    items.filter { article ->
                        query?.let {
                            article.title?.contains(
                                it,
                                ignoreCase = true
                            )
                        } == true || query?.let {
                            article.description?.contains(
                                it,
                                ignoreCase = true
                            )
                        } == true
                    }
                adapter.updateArticles(filteredNews)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                var filteredNews =
                    items.filter { article ->
                        newText?.let {
                            article.title?.contains(
                                it,
                                ignoreCase = true
                            )
                        } == true || newText?.let {
                            article.description?.contains(
                                it,
                                ignoreCase = true
                            )
                        } == true
                    }
                adapter.updateArticles(filteredNews)
                return false
            }
        })
    }

    private fun fetchData() {
        val newsRepository = NewsRepository()

        lifecycleScope.launch {
            try {
                val fetchedItems = newsRepository.getTopHeadlines()
                items = fetchedItems
                adapter.updateArticles(fetchedItems)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    class NewsAdapter(private var articles: List<NewsResponse.Article>) :
        RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val article = articles[position]
            holder.titleTextView.text = article.title
            holder.descriptionTextView.text = article.description
            holder.link.text = article.url
        }

        override fun getItemCount(): Int {
            return articles.size
        }

        fun updateArticles(newArticles: List<NewsResponse.Article>) {
            articles = newArticles
            notifyDataSetChanged()
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
            val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
            val link: TextView = itemView.findViewById(R.id.link)
        }
    }

    object ApiKey {
        const val KEY = BuildConfig.API_KEY
    }

    interface NewsApiService {
        @GET("top-headlines")
        suspend fun getTopHeadlines(
            @Query("country") country: String = "us",
            @Query("apiKey") apiKey: String = ApiKey.KEY
        ): Response<NewsResponse>
    }

    data class NewsResponse(
        val status: String,
        val totalResults: Int,
        val articles: List<Article>
    ){
        data class Article(
            val source: Source?,
            val author: String?,
            val title: String?,
            val description: String?,
            val url: String?,
            val urlToImage: String?,
            val publishedAt: String?,
            val content: String?
        ) {
            data class Source(
                val id: String?,
                val name: String?
            )
        }
    }

    class NewsRepository() {
        var retrofit = Retrofit
            .Builder()
            .baseUrl("https://newsapi.org/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        private val apiService: NewsApiService = retrofit.create(NewsApiService::class.java)

        suspend fun getTopHeadlines(): List<NewsResponse.Article> {
            val response = apiService.getTopHeadlines()
            if (response.isSuccessful) {
                return response.body()?.articles ?: emptyList()
            } else {
                throw ApiException(response.code(), response.message())
            }
        }
    }

    class ApiException(code: Int, message: String) : Exception("API request failed with code $code: $message")
}