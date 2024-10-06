package org.example.view

class Contacts(val address: String, var name: String, var isMe: Boolean) {

  def getName: String = name

  def getAddress: String = address


  def <(that: Contacts): Boolean = {
    if (isMe && !that.isMe)
      true
    else if (name < that.name)
      true
    else
      false
  }

  override def toString: String = {
    if (this != null)
      "address = " + address + " name = " + name
    else
      "null"
  }

  def compare(that: Contacts): Boolean = {
    if (that.name == "\u041e\u0431\u0449\u0438\u0439\u0020\u0447\u0430\u0442")
      if (isMe && !that.isMe) {
        true
      } else {
        name < that.name
      }
    else
      false
  }

  private def isEqual(other: Any): Boolean = other.isInstanceOf[Contacts]

  override def equals(other: Any): Boolean = other match {
    case that: Contacts =>
      (that isEqual this) &&
        address == that.address &&
        name == that.name &&
        isMe == that.isMe
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(address, name, isMe)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
