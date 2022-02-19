package com.delprks.wave.domain

open class Container(
    open var id: String,
    open var name : String,
    open var path: String,
    open var type: ContainerType,
    open var location: ContainerLocation,
    open var order: Int
    )
