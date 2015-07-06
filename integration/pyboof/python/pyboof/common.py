class JavaWrapper:
    def set_java_object(self, obj ):
        self.java_obj = obj

    def get_java_object(self):
        return self.java_obj

    def __str__(self):
        return self.java_obj.toString()