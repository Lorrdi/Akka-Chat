package org.example.view

@SerialVersionUID(100L)
class Message extends Serializable {
  private var PostText: String = _
  private var From: String = _
  private var To: String = _
  PostText = "\u0412\u0432\u0435\u0434\u0438\u0442\u0435\u0020\u0442\u0435\u043a\u0441\u0442\u0020\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u044f"


  def this(postText: String, from: String, to: String) = {
    this()
    From = from
    To = to
    PostText = postText
  }

  def getTo: String = To

  def getFrom: String = From

  def getPostText: String = PostText

  override def toString: String = {
    From + " : " + To + " | " + PostText
  }

}
