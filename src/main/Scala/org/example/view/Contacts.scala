package org.example.view

case class Contacts(address: String, name: String, isMe: Boolean) extends Ordered[Contacts] {

  def compare(that: Contacts): Int = {
    if (that.name == "Общий чат") {
      if (isMe && !that.isMe) -1 else name.compare(that.name)
    } else {
      1
    }
  }

  override def toString: String = s"address = $address name = $name"
}
