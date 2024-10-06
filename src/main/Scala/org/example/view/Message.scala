package org.example.view

@SerialVersionUID(100L)
class Message(
  private var postText: String = "Введите текст сообщения",
  private var from: String = "",
  private var to: String = ""
) extends Serializable {

  def getTo: String = to
  def getFrom: String = from
  def getPostText: String = postText

  override def toString: String = s"$from : $to | $postText"
}
