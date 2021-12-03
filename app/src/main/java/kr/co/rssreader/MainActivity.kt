package kr.co.rssreader

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kr.co.rssreader.adapter.ArticleAdapter
import kr.co.rssreader.model.Article
import kr.co.rssreader.model.Feed
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import javax.xml.parsers.DocumentBuilderFactory

class MainActivity : AppCompatActivity() {
    //val dispatcher = newSingleThreadContext(name = "ServiceCall")

    private lateinit var articles: RecyclerView
    private lateinit var viewAdapter: ArticleAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    val dispatcher = newFixedThreadPoolContext(2, "IO")
    val factory = DocumentBuilderFactory.newInstance()

    val feeds = listOf(
        Feed("npr","https://www.npr.org/rss/rss.php?id=1001"),
        Feed("cnn","http://rss.cnn.com/rss/cnn_topstories.rss"),
        Feed("fox","http://feeds.foxnews.com/foxnews/politics?format=xml"),
        Feed("inv","htt:asdad.ads")
    )

    private fun asyncFetchArticles(feed: Feed,
                                    dispatcher: CoroutineDispatcher) = GlobalScope.async(dispatcher){
        delay(1000)
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed.url)
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
            .map{ news.childNodes.item(it) }
            .filter{ Node.ELEMENT_NODE == it.nodeType }
            .map{ it as Element }
            .filter{ "item" == it.tagName}
            .map{
                //it.getElementsByTagName("title").item(0).textContent
                val title = it.getElementsByTagName("title")
                    .item(0)
                    .textContent
                var summary = it.getElementsByTagName("description")
                    .item(0)
                    .textContent
                if(!summary.startsWith("<div")
                    && summary.contains("<div")){
                    summary = summary.substring(0, summary.indexOf("<div"))
                }
                Article(feed.name, title, summary)
            }
    }

    private fun asyncFetchHeadlines(feed: Feed,
            dispatcher: CoroutineDispatcher) = GlobalScope.async(dispatcher){
        val builder = factory.newDocumentBuilder()
        val xml = builder.parse(feed.url)
        val news = xml.getElementsByTagName("channel").item(0)

        (0 until news.childNodes.length)
            .map{ news.childNodes.item(it) }
            .filter{ Node.ELEMENT_NODE == it.nodeType }
            .map{ it as Element }
            .filter{ "item" == it.tagName}
            .map{
                it.getElementsByTagName("title").item(0).textContent
            }
    }

//    private fun loadNews(){
//        GlobalScope.launch(dispatcher){
//        val headlines = fetchRssHeadlines()
//        val newsCount = findViewById<TextView>(R.id.newsCount)
//        GlobalScope.launch(Dispatchers.Main) {
//                newsCount.text = "Found ${headlines.size} News"
//            }
//        }
//    }

    private fun asyncLoadNews() = GlobalScope.launch{
        val requests= mutableListOf<Deferred<List<Article>>>()

        feeds.mapTo(requests){
            asyncFetchArticles(it, dispatcher)
        }
        requests.forEach{
            it.join()
        }

        val articles = requests
            .filter{ !it.isCancelled}
            .flatMap { it.getCompleted()
        }

        val failed = requests
            .filter { it.isCancelled }
            .size

//        val newsCount = findViewById<TextView>(R.id.newsCount)
//        val warnings = findViewById<TextView>(R.id.warnings)
//        val obtained = requests.size - failed
        launch(Dispatchers.Main) {
//            newsCount.text = "Found ${articles.size} News" +
//                                "in $obtained feeds"
//            if(failed > 0){
//                warnings.text = "Failed to fetch $failed feeds"
//            }
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            viewAdapter.add(articles)
        }
    }

//    private fun fetchRssHeadlines(): List<String>{
//        val builder = factory.newDocumentBuilder()
//        val xml = builder.parse("https://www.npr.org/rss/rss.php?id=1001")
//        val news = xml.getElementsByTagName("channel").item(0)
//        return (0 until news.childNodes.length)
//            .map{ news.childNodes.item(it) }
//            .filter{ Node.ELEMENT_NODE == it.nodeType }
//            .map{ it as org.w3c.dom.Element }
//            .filter{ "item" == it.tagName}
//            .map{
//                it.getElementsByTagName("title").item(0).textContent
//            }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        GlobalScope.launch(dispatcher) {
//            loadNews()
//        }

        viewManager = LinearLayoutManager(this)
        viewAdapter = ArticleAdapter()
        articles = findViewById<RecyclerView>(R.id.articles).apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        asyncLoadNews()
    }

}